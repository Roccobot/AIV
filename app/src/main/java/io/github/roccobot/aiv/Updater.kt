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
 * L'aggiornamento da sé di AIV GitHub.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * Scelte C1 e D1 dell'utente, 5.60: AIV scarica e installa con la sua conferma; controlla
 * all'apertura dell'app, al massimo una volta al giorno, senza lavoro in sottofondo e senza
 * notifiche.
 * Solo AIV GitHub ([Store.updates]): AIV Play lo aggiorna il Play Store.
 */
object Updater {

    /** L'ultima release del repository, come la serve l'API di GitHub senza token. */
    private const val LATEST = "https://api.github.com/repos/Roccobot/AIV/releases/latest"

    /**
     * L'APK di AIV GitHub fra gli asset di una release.
     *
     * ⚠️ **Una release ha due APK**, e `AIV-Play-x.xx.apk` non va mai scelto: è un'altra app, con
     * un altro identificativo. Il nome è quello che scrive `release.yml`.
     */
    private val APK = Regex("""^AIV-\d+\.\d+\.apk$""")

    /** Una release più nuova dell'app installata, con quello che serve a scaricarla e verificarla. */
    data class Release(
        val version: String,
        val url: String,
        val size: Long,
        /** Lo SHA-256 che GitHub dichiara per l'asset, in esadecimale minuscolo. */
        val sha256: String
    )

    /**
     * La release offerta, che `UpdateDialog` mostra alla radice dell'app.
     *
     * È qui e non in una schermata perché la mettono in scena sia il controllo all'apertura sia
     * 'Controlla ora' nelle impostazioni, e la finestra deve sopravvivere alla schermata.
     */
    var offer by mutableStateOf<Release?>(null)

    /**
     * Dove girano lo scaricamento e l'installatore, per tutta la vita del processo.
     *
     * ⚠️ **Non nello scope della finestra**: la prima cosa che il lavoro fa è chiuderla, e con lei
     * si annullerebbe lo scope; lo scaricamento arriverebbe comunque in fondo (una lettura
     * bloccante non si ferma) e l'installatore non si aprirebbe, senza avvisi. Presidiato da
     * `AggiornaTest`.
     */
    private val work = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Avvia [install] dove la chiusura della finestra non può fermarlo. */
    fun start(context: Context, release: Release) {
        val app = context.applicationContext
        work.launch { install(app, release) }
    }

    /**
     * Legge l'ultima release dalla risposta dell'API: `null` quando non ha un APK utilizzabile.
     *
     * ⚠️ **Senza impronta non c'è release**: la verifica scelta dall'utente è la firma e
     * l'impronta, e un APK che non si può confrontare con quello che GitHub dichiara non si
     * installa.
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
     * Se [remote] è più nuova di [local], tutte e due SlimVer (`x.xx`).
     *
     * ⚠️ **Si confrontano come decimali, non come testo**: come stringhe `10.00` verrebbe prima
     * di `9.99`. Un numero illeggibile non è mai più nuovo.
     */
    internal fun newer(remote: String, local: String): Boolean {
        val a = remote.toBigDecimalOrNull() ?: return false
        val b = local.toBigDecimalOrNull() ?: return false
        return a > b
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()

    /**
     * Se tocca al controllo automatico: una volta per giorno di calendario (scelta D1).
     *
     * Il giorno di calendario e non 24 ore: chi apre l'app ogni mattina all'incirca alla stessa
     * ora salterebbe un giorno su due, aprendo un minuto prima dello scadere.
     */
    internal fun due(last: Long?, today: Long): Boolean = last != today

    private fun prefs(context: Context) =
        context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    /**
     * Chiede a GitHub l'ultima release, e la offre quando è più nuova di questa app.
     *
     * @param asked `true` per 'Controlla ora': gira anche se il controllo di oggi è già fatto, e
     *   dice l'esito ('AIV è aggiornata', o il fallimento). Il controllo automatico tace se non
     *   c'è niente da offrire: un telefono senza rete all'apertura non è una notizia.
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
     * Scarica [release], la verifica e la passa all'installatore di Android.
     *
     * ⚠️ **Le due verifiche vengono prima dell'installatore**: lo SHA-256 del file deve essere
     * quello che GitHub dichiara, e l'APK deve essere firmato con la chiave dell'app installata.
     * Un file che ne fallisce una si cancella, e l'installatore non lo vede: darebbe un errore suo
     * senza dire perché.
     * Il permesso di installare app si chiede prima dello scaricamento: Android lo concede da una
     * sua pagina e la finestra resta, quindi al ritorno si tocca di nuovo 'Aggiorna'.
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

    /** Il nome dell'APK scaricato di [version] nella cache. */
    internal fun apkName(version: String): String = "AIV-$version.apk"

    /**
     * I file della cache degli aggiornamenti che non servono più.
     *
     * Un APK resta finché si può ancora installare: quello di una versione più nuova di questa
     * app, o quello che si sta scaricando ([keep]). Gli altri sono gli aggiornamenti già
     * installati, circa undici megabyte l'uno.
     */
    internal fun stale(names: List<String>, current: String, keep: String?): List<String> =
        names.filter { name ->
            if (name == keep) return@filter false
            if (keep != null) return@filter true
            val version = name.removePrefix("AIV-").removeSuffix(".apk")
            name == version || !newer(version, current)
        }

    /** Cancella i file [stale] della cache degli aggiornamenti. */
    internal fun sweep(context: Context, keep: String?) {
        val dir = File(context.cacheDir, DIR)
        val names = dir.list()?.toList() ?: return
        stale(names, BuildConfig.VERSION_NAME, keep).forEach { File(dir, it).delete() }
    }

    /** Il corpo di [url], come testo. */
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
     * Scrive [url] in [file], e risponde con lo SHA-256 di quello che ha scritto.
     *
     * L'impronta si calcola mentre i byte arrivano, senza una seconda lettura del file.
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
     * Se l'APK in [file] è questa app, firmata con la chiave di quella installata.
     *
     * ⚠️ **Si controlla anche il nome del pacchetto**: un file con la stessa chiave ma
     * l'identificativo di AIV Play sarebbe un'altra app, e l'installatore la metterebbe accanto a
     * questa.
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
     * I firmatari di un pacchetto.
     *
     * Prima `signingInfo`, poi `signatures` come ripiego: su alcune versioni di Android le
     * informazioni di un archivio lasciano vuoto `signingInfo`, e il campo vecchio dice ancora chi
     * l'ha firmato.
     */
    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Set<Signature> =
        info.signingInfo?.apkContentsSigners?.toSet()?.takeIf { it.isNotEmpty() }
            ?: info.signatures?.toSet().orEmpty()

    /** Se due insiemi di firmatari sono uguali e non vuoti. */
    internal fun sameSigners(a: Set<Signature>, b: Set<Signature>): Boolean = a.isNotEmpty() && a == b

    private const val LAST = "checked-day"
    private const val DIR = "updates"
    private const val TIMEOUT_MS = 15_000
    private const val BUFFER = 64 * 1024
}

/**
 * La finestra che offre una release più nuova: 'AIV x.xx è disponibile', con 'Aggiorna' e 'Più
 * tardi'.
 *
 * Non è modale (`lowered` con un gesto di chiusura): non chiede un input, e un tocco fuori vale
 * 'Più tardi' (`Rules.md` § '👆 Che cosa fa il tocco FUORI da una finestra').
 * 'Più tardi' aspetta domani: il controllo automatico ha già contato oggi, quindi l'offerta
 * torna all'apertura del giorno dopo, o subito con 'Controlla ora'.
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

package io.github.roccobot.aiv

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/*
 * 'Elimina' in AIV Play, attraverso il MediaStore.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * Scelte A1 e B1 dell'utente: da Android 11 'Elimina' sposta i file nel cestino di sistema
 * (`createTrashRequest`), che li tiene circa 30 giorni (`MediaColumns.DATE_EXPIRES`); su Android 9
 * e 10, dove quel cestino non esiste, li elimina definitivamente dopo la conferma di AIV.
 * ⚠️ **Android chiede ogni volta, e AIV non può evitarlo**: solo la galleria del produttore del
 * telefono sposta nel cestino senza la finestra di sistema. Su Android 10 un file di un'altra app
 * chiede una volta per file (`RecoverableSecurityException`); su Android 9 il permesso di scrittura
 * si chiede una volta.
 * AIV GitHub non passa mai di qui: scrive con `java.io.File`, e il suo cestino è una sua cartella.
 * La pagina del cestino di AIV Play legge il cestino di sistema ([MediaDelete.trashed]): 'Ripristina'
 * rimette i file con la stessa richiesta spenta, 'Elimina' e 'Svuota il cestino' li eliminano
 * definitivamente con `createDeleteRequest`. Ognuna passa dalla finestra di Android, e AIV non ne
 * aggiunge altre.
 */
object MediaDelete {

    /**
     * Le righe del MediaStore di [uris]: un `content://` passa com'è, un `file://` si cerca per
     * percorso. Un file che il MediaStore non conosce non ha riga, e conta come non riuscito.
     */
    fun rows(context: Context, uris: List<Uri>): List<Uri> = uris.mapNotNull { uri ->
        when (uri.scheme?.lowercase()) {
            "content" -> uri
            "file" -> uri.path?.let { Tree.contentUri(context, File(it)) }
            else -> null
        }
    }

    /** La finestra di sistema che sposta [rows] nel suo cestino. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun trashRequest(context: Context, rows: List<Uri>): IntentSender =
        MediaStore.createTrashRequest(context.contentResolver, rows, true).intentSender

    /** La finestra di sistema che toglie [rows] dal suo cestino e le rimette dov'erano. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun restoreRequest(context: Context, rows: List<Uri>): IntentSender =
        MediaStore.createTrashRequest(context.contentResolver, rows, false).intentSender

    /** La finestra di sistema che elimina [rows] definitivamente, anche dal suo cestino. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun deleteRequest(context: Context, rows: List<Uri>): IntentSender =
        MediaStore.createDeleteRequest(context.contentResolver, rows).intentSender

    /**
     * Le immagini e i video nel cestino di sistema, dall'ultimo eliminato: la pagina del cestino di
     * AIV Play (scelta A1 dell'utente).
     *
     * L'ordine è la data di scadenza, decrescente: il sistema la fissa quando il file entra nel
     * cestino, quindi è l'ordine di eliminazione, lo stesso del cestino di AIV GitHub.
     * Una domanda per raccolta, immagini e video, perché ogni riga abbia l'indirizzo della sua
     * raccolta, che le finestre di sistema vogliono; i file di altri tipi non entrano nel cestino di
     * AIV. I PSD restano fuori, come in tutta l'app (`MediaFiles.ignored`).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun trashed(context: Context): List<Uri> = withContext(Dispatchers.IO) {
        val args = android.os.Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            putString(
                android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "${MediaStore.MediaColumns.DATE_EXPIRES} DESC"
            )
        }
        val columns = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_EXPIRES,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE
        )
        listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).flatMap { collection ->
            runCatching {
                context.contentResolver.query(collection, columns, args, null)?.use { c ->
                    buildList {
                        while (c.moveToNext()) {
                            if (MediaFiles.ignored(c.getString(2), c.getString(3))) continue
                            add(c.getLong(1) to android.content.ContentUris.withAppendedId(collection, c.getLong(0)))
                        }
                    }
                }
            }.getOrNull().orEmpty()
        }.sortedByDescending { it.first }.map { it.second }
    }

    /**
     * Elimina definitivamente [uris], sotto Android 11.
     *
     * Su Android 10 un file di un'altra app risponde con una [RecoverableSecurityException], la cui
     * finestra mostra [ask]; dopo un sì la stessa eliminazione si riprova. Su Android 9 prima viene
     * il permesso di scrittura, da [grant], e senza non si elimina niente.
     */
    suspend fun forGood(
        context: Context,
        uris: List<Uri>,
        ask: SystemAsk
    ): FileTree.Outcome {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !ask.grant(WRITE)) {
            return FileTree.Outcome(0, uris.size)
        }
        val rows = withContext(Dispatchers.IO) { rows(context, uris) }
        var done = 0
        for (row in rows) {
            val gone = try {
                remove(context, row)
            } catch (e: SecurityException) {
                val consent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    (e as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender
                } else {
                    null
                }
                consent != null && ask.ask(consent) && runCatching { remove(context, row) }.getOrDefault(false)
            }
            if (gone) done++
        }
        return FileTree.Outcome(done, uris.size - done)
    }

    private suspend fun remove(context: Context, row: Uri): Boolean =
        withContext(Dispatchers.IO + NonCancellable) { context.contentResolver.delete(row, null, null) > 0 }

    /** Il permesso che Android 9 vuole per eliminare un file di un'altra app, dichiarato fino ad Android 9. */
    @Suppress("DEPRECATION")
    private const val WRITE = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
}

/**
 * Una finestra di sistema che risponde sì o no, attesa da un lavoro sospeso: la richiesta del
 * cestino, il consenso di Android 10, il permesso di Android 9.
 *
 * ⚠️⚠️ **Vive in cima a `FileJobDialogs`, mai nel ramo di un lavoro**: il lavoro continua dopo che
 * la sua finestra si è chiusa, e un launcher tolto dalla composizione non consegnerebbe mai la
 * risposta.
 * ⚠️ **Se l'attività è ricreata con la finestra di sistema aperta, la risposta si perde**: la
 * finestra è un'attività a sé, e il launcher nuovo non conosce la domanda vecchia. Per questo la
 * schermata che se ne va risponde 'no' ([rememberSystemAsk]), e il lavoro conta quel file come non
 * fatto e termina.
 */
class SystemAsk internal constructor(
    private val context: Context,
    private val window: (IntentSenderRequest) -> Unit,
    private val permission: (String) -> Unit
) {
    internal var pending: CompletableDeferred<Boolean>? = null

    /** Mostra la finestra di [sender] e risponde se la persona ha detto sì. */
    suspend fun ask(sender: IntentSender): Boolean = await {
        window(IntentSenderRequest.Builder(sender).build())
    }

    /** Chiede [name] quando manca, e risponde se è concesso. */
    suspend fun grant(name: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED) return true
        return await { permission(name) }
    }

    /**
     * ⚠️ **Si chiama sul thread principale**, dove un launcher vuole essere: i lavori girano in
     * `FileWork` e in un `LaunchedEffect`, tutti sul thread principale. Un passaggio esplicito al
     * dispatcher principale qui aspetterebbe per sempre sotto un chiamante che blocca quel thread.
     */
    private suspend fun await(launch: () -> Unit): Boolean {
        val answer = CompletableDeferred<Boolean>()
        pending = answer
        if (runCatching { launch() }.isFailure) {
            pending = null
            return false
        }
        return answer.await()
    }

    internal fun answer(yes: Boolean) {
        pending?.complete(yes)
        pending = null
    }
}

/** Il [SystemAsk] di una schermata, coi suoi due launcher. */
@Composable
fun rememberSystemAsk(): SystemAsk {
    val context = LocalContext.current
    val holder = remember { arrayOfNulls<SystemAsk>(1) }
    val window = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        holder[0]?.answer(it.resultCode == Activity.RESULT_OK)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        holder[0]?.answer(it)
    }
    val ask = remember(window, permission) {
        SystemAsk(context, { window.launch(it) }, { permission.launch(it) }).also { holder[0] = it }
    }
    // ⚠️ Quando la schermata se ne va (l'attività ricreata con la finestra di sistema aperta), la
    // domanda ancora in attesa riceve 'no': i lavori vivono in `FileWork` e non terminano con la
    // schermata, quindi senza questo il lavoro aspetterebbe una risposta che non arriva.
    DisposableEffect(ask) { onDispose { ask.answer(false) } }
    return ask
}

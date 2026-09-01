package io.github.roccobot.aiv

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri

/**
 * Chi modifica una fotografia: l'editor di casa, o un'app del telefono.
 *
 * ⚠️⚠️ **L'ELENCO SI CHIEDE AL SISTEMA E NON SI SCRIVE**: quali app sappiano modificare
 * un'immagine lo sa `PackageManager`, e ogni elenco scritto a mano sarebbe vecchio il giorno
 * dopo. Si domanda chi risponde a `ACTION_EDIT` sul tipo di [MIME], che è il modo con cui
 * Android chiede proprio questo.
 *
 * ⚠️⚠️ **QUI DENTRO IL TIPO NON SI SCRIVE PER ESTESO, e non è pignoleria**: i commenti a
 * blocco di Kotlin si ANNIDANO, quindi la barra seguita dall'asterisco di un tipo con jolly
 * apre un commento dentro il commento, e la chiusura del KDoc chiude quello invece di questo.
 * Il file smette di compilare con un 'Unclosed comment' segnalato **all'ultima riga**, cioè
 * lontanissimo dalla causa. Già successo qui.
 * ⚠️⚠️ **E SERVE IL BLOCCO `<queries>` NEL MANIFEST**: da Android 11 un'app non vede le altre
 * se non dichiara che cosa cerca. Senza, questa funzione risponde **elenco vuoto** su un
 * telefono pieno di editor, e il difetto si vede solo su Android 11 in su, cioè non sul
 * telefono di chi scrive il codice se ne ha uno vecchio.
 *
 * ⚠️ **La scelta si ricorda come TESTO e non come oggetto**: nelle impostazioni ci va una
 * stringa, e un `ComponentName` si scrive e si rilegge con `flattenToString`. Il valore
 * [INTERNAL] è l'unico che non è un componente, ed è l'editor di casa.
 */
object Editors {

    /** Il valore che vuol dire 'l'editor dentro AIV'. Vedi `Settings.editorApp`. */
    const val INTERNAL = "interno"

    /**
     * Il prefisso che marca una scelta raggiunta con la CONDIVISIONE invece che con la
     * modifica.
     *
     * ⚠️⚠️ **STA DENTRO L'IDENTIFICATIVO SALVATO, e non in un campo a parte**: la scelta
     * vive nelle impostazioni come **una stringa** (`Settings.editorApp`), e aggiungere un
     * secondo campo vorrebbe dire due valori da tenere d'accordo per sempre. Un identificativo
     * senza prefisso resta quello che era, quindi le scelte già salvate continuano a valere.
     */
    const val SHARED = "send:"

    /** Una voce dell'elenco: che cosa scegliere, come si chiama, e la sua icona. */
    data class Choice(val id: String, val label: String, val icon: Drawable?) {

        /** Se ci si arriva condividendo la fotografia invece che aprendola per modificarla. */
        val shared: Boolean get() = id.startsWith(SHARED)
    }

    /**
     * Le app che sanno modificare un'immagine, in ordine alfabetico.
     *
     * ⚠️ **Senza l'editor di casa**: quello lo mette la schermata, in cima, perché è l'unico
     * che c'è sempre e non dipende da che cosa ha installato l'utente.
     */
    fun installed(context: Context): List<Choice> {
        val pm = context.packageManager
        /*
         * ⚠️⚠️ **SI DOMANDA IN TRE MODI E SI UNISCE, ed è la correzione del 2026-08-31**
         * (segnalazione dell'utente: Photo Editor e Magic Eraser installati e non fra le
         * scelte). Il difetto era doppio, e una sola delle due cause non bastava a spiegarlo:
         * - **il TIPO da solo non basta**: un filtro che dichiara anche `android:scheme` non
         *   risponde a un intent senza indirizzo, quindi l'app resta invisibile. Le due righe
         *   con l'indirizzo finto coprono quel caso, e l'indirizzo non si apre mai: serve
         *   soltanto a far combaciare il filtro.
         * - **`MATCH_DEFAULT_ONLY` esclude chi non dichiara `CATEGORY_DEFAULT`**, e sono
         *   tanti. Qui non serve a niente: quella categoria conta per la risoluzione
         *   *implicita*, e [open] parte con un **componente esplicito**, dove non è richiesta.
         * ⚠️ **L'elenco vuoto non è un errore visibile**, ed è la ragione per cui il difetto
         * poteva restare: senza nessuna delle due correzioni il selettore mostra l'editor di
         * casa e basta, che è esattamente quello che farebbe su un telefono spoglio.
         */
        val found = ask(pm, Intent.ACTION_EDIT, MIME)
        val editors = found.toChoices(context, pm, "")

        /*
         * ⚠️⚠️ **CHI NON SI DICHIARA EDITOR SI RAGGIUNGE CONDIVIDENDO, dalla 1.11** (prova
         * dell'utente: Magic Eraser non compare in nessun 'Modifica', e compare fra i primi
         * risultati di 'Condividi'). Non è un difetto della domanda: quell'app **non ha** un
         * filtro `ACTION_EDIT`, quindi nemmeno Android la considera un editor, e nessuna
         * query di modifica la troverà mai. L'unica via che esiste è quella che l'app stessa
         * dichiara.
         * ⚠️⚠️ **IL FILTRO CHE RENDE L'ELENCO USABILE: si toglie chi accetta anche il TESTO.**
         * Domandare chi riceve un'immagine risponde con la messaggistica, la posta, il cloud,
         * il Bluetooth: quaranta voci di cui tre sono foto-app. Un'app che tratta fotografie
         * accetta immagini e **non** testo semplice; una che serve a mandare roba in giro
         * accetta tutti e due. Sottraendo il secondo elenco dal primo resta quasi solo quello
         * che interessa.
         * ⚠️ **È un'euristica e si dichiara**: può lasciare fuori un editor che accetta anche
         * testo (raro) e può tenere dentro un archivio in rete. Per questo le due famiglie
         * restano **separate** nel selettore invece di essere mescolate: chi sceglie vede da
         * quale delle due sta pescando.
         */
        val shares = ask(pm, Intent.ACTION_SEND, MIME).toChoices(context, pm, SHARED)
        val talkers = ask(pm, Intent.ACTION_SEND, TEXT)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
        val known = editors.map { it.id.substringAfterLast(':') }.toSet()
        val others = shares.filterNot {
            it.id.removePrefix(SHARED).substringBefore('/') in talkers ||
                it.id.removePrefix(SHARED) in known
        }
        return editors + others
    }

    /** Chi risponde a questa azione con questo tipo, nelle tre forme che i filtri assumono. */
    private fun ask(pm: PackageManager, action: String, type: String): List<ResolveInfo> {
        val asks = listOf(
            Intent(action).setType(type),
            Intent(action).setDataAndType(SAMPLE_CONTENT, type),
            Intent(action).setDataAndType(SAMPLE_FILE, type)
        )
        return asks.flatMap { one ->
            runCatching {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(one, 0)
            }.getOrNull().orEmpty()
        }
    }

    /** Da risposte del sistema a voci dell'elenco, in ordine alfabetico. */
    private fun List<ResolveInfo>.toChoices(
        context: Context,
        pm: PackageManager,
        mark: String
    ): List<Choice> = asSequence()
        // ⚠️ AIV stessa non compare fra le scelte: sceglierla vorrebbe dire chiedere a
        // questa app di aprire questa app, che non è quello che la voce promette.
        .filter { it.activityInfo?.packageName != context.packageName }
        .mapNotNull { info ->
            val act = info.activityInfo ?: return@mapNotNull null
            Choice(
                id = mark + ComponentName(act.packageName, act.name).flattenToString(),
                label = runCatching { info.loadLabel(pm).toString() }.getOrNull()
                    ?: act.packageName,
                icon = runCatching { info.loadIcon(pm) }.getOrNull()
            )
        }
        .distinctBy { it.id }
        .sortedBy { it.label.lowercase() }
        .toList()

    /**
     * Come si chiama quello scelto, e `null` se non è più installato.
     *
     * ⚠️ **La disinstallazione va vista, non subita**: se l'app scelta sparisce, la voce nelle
     * impostazioni deve tornare a dire 'nessuna' invece di mostrare il nome di una cosa che
     * non c'è, e il menu deve tornare a chiedere.
     */
    fun labelOf(context: Context, id: String): String? {
        if (id.isBlank()) return null
        if (id == INTERNAL) return context.getString(R.string.editor_internal)
        return installed(context).firstOrNull { it.id == id }?.label
    }

    /**
     * Manda la fotografia all'app scelta.
     *
     * ⚠️⚠️ **IL PERMESSO DI SCRITTURA SI CONCEDE INSIEME ALL'INDIRIZZO**: senza
     * `FLAG_GRANT_WRITE_URI_PERMISSION` l'editor apre la fotografia e poi non riesce a
     * salvarla, e l'errore lo dà lui, in una schermata che non è nostra. Chi lo vede non ha
     * modo di risalire ad AIV.
     * ⚠️ **Il componente è esplicito**, ed è tutto il senso di ricordare la scelta: con un
     * intent generico Android richiederebbe di scegliere ogni volta, che è la cosa che la
     * memoria della scelta doveva togliere.
     */
    fun open(context: Context, uri: Uri, id: String): Boolean {
        val shared = id.startsWith(SHARED)
        val component = ComponentName.unflattenFromString(id.removePrefix(SHARED)) ?: return false
        /*
         * ⚠️⚠️ **CONDIVIDERE NON È MODIFICARE, e la differenza si vede nel risultato**: con
         * `ACTION_EDIT` l'app riceve **la fotografia** e la riscrive dov'era; con
         * `ACTION_SEND` ne riceve una copia e salva **dove decide lei**, di solito in una
         * cartella sua. L'originale non viene toccato.
         * ⚠️ **Alla condivisione non si dà il permesso di SCRITTURA**, e non è una svista:
         * quell'app non deve riscrivere niente, e un permesso che non serve non si concede.
         */
        val intent = if (shared) {
            Intent(Intent.ACTION_SEND).apply {
                type = MIME
                putExtra(Intent.EXTRA_STREAM, uri)
                this.component = component
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_EDIT).apply {
                setDataAndType(uri, MIME)
                this.component = component
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        // ⚠️ Il falso copre tutti i modi di non partire, non solo l'app sparita: chi chiama
        // deve solo sapere se dire all'utente che non si è aperto niente.
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }

    /** Il tipo che si chiede e si dichiara: uno solo, e sta scritto in un posto solo. */
    private const val MIME = "image/*"

    /** Il tipo che serve solo a **escludere**: chi accetta anche questo non è una foto-app. */
    private const val TEXT = "text/plain"

    /**
     * Due indirizzi finti, per la sola domanda al sistema.
     *
     * ⚠️⚠️ **NON SI APRONO MAI, e non devono esistere**: servono a far combaciare i filtri
     * che pretendono uno schema, e la risposta che interessa è *quale app*, non *che cosa
     * c'è là dentro*. Un indirizzo vero renderebbe la domanda dipendente da una fotografia
     * che al momento della domanda potrebbe non esserci.
     */
    private val SAMPLE_CONTENT: Uri = Uri.parse("content://media/external/images/media/1")
    private val SAMPLE_FILE: Uri = Uri.parse("file:///storage/emulated/0/Pictures/x.jpg")
}

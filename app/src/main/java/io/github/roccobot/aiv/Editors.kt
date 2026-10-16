package io.github.roccobot.aiv

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri

/**
 * Chi modifica un'immagine: l'editor interno, o un'app del telefono.
 *
 * L'elenco si chiede al sistema: quali app sappiano modificare un'immagine lo sa
 * `PackageManager`, e un elenco scritto a mano invecchierebbe subito. Si domanda chi risponde a
 * `ACTION_EDIT` sul tipo di [MIME].
 *
 * ⚠️⚠️ **Nei commenti di questo file il tipo non si scrive per esteso**: i commenti a blocco di
 * Kotlin si annidano, quindi la barra seguita dall'asterisco di un tipo con jolly apre un
 * commento dentro il commento. Il file smette di compilare con un 'Unclosed comment' segnalato
 * all'ultima riga, lontano dalla causa.
 * ⚠️ **Serve il blocco `<queries>` nel manifest**: da Android 11 un'app non vede le altre se non
 * dichiara che cosa cerca, e senza questa funzione risponde un elenco vuoto.
 *
 * La scelta si ricorda come stringa: un `ComponentName` si scrive e si rilegge con
 * `flattenToString`. [INTERNAL] è il solo valore che non è un componente, ed è l'editor
 * dell'app (con [FULL], il valore vecchio del completo).
 */
object Editors {

    /**
     * Il valore che vuol dire 'l'editor dentro AIV', cioè la voce 'Editor interno'.
     * Vedi `Settings.editorApp`.
     *
     * ⚠️ **Una voce sola per i due editor dell'app** (voce `5.00-02` dell'utente, 5.01): sotto
     * Android 13 si apre l'editor semplice, da Android 13 in su il completo. Lo decide il
     * telefono al momento di aprirlo ([advancedEditorAvailable]), non la scelta salvata.
     */
    const val INTERNAL = "interno"

    /**
     * Il valore vecchio dell'editor completo dentro AIV.
     *
     * ⚠️ **Nessuno lo scrive più, ma si legge ancora**: è la scelta che i telefoni hanno già
     * salvato, e vale come [INTERNAL] ([isInternal]). Un valore salvato non si traduce e non si
     * rinomina.
     */
    const val FULL = "completo"

    /**
     * Se una scelta salvata è l'editor interno: [INTERNAL] o [FULL]. È il solo posto che conosce
     * tutti e due i valori.
     */
    fun isInternal(id: String): Boolean = id == INTERNAL || id == FULL

    /**
     * Il prefisso che marca una scelta raggiunta con la condivisione e non con la modifica.
     *
     * È dentro l'identificativo salvato perché la scelta è una stringa sola
     * (`Settings.editorApp`): un secondo campo sarebbe un secondo valore da tenere d'accordo. Un
     * identificativo senza prefisso resta valido.
     */
    const val SHARED = "send:"

    /** Una voce dell'elenco: che cosa scegliere, come si chiama, e la sua icona. */
    data class Choice(val id: String, val label: String, val icon: Drawable?) {

        /** Se ci si arriva condividendo l'immagine e non aprendola per modificarla. */
        val shared: Boolean get() = id.startsWith(SHARED)
    }

    /**
     * Le app che sanno modificare un'immagine, in ordine alfabetico.
     *
     * Senza l'editor interno: lo mette la schermata, in cima, perché c'è sempre.
     */
    fun installed(context: Context): List<Choice> {
        val pm = context.packageManager
        /*
         * ⚠️ **Si domanda in tre modi e si uniscono le risposte** ([ask]):
         * - il solo tipo non basta: un filtro che dichiara anche `android:scheme` non risponde a
         *   un intent senza indirizzo. Gli indirizzi finti coprono quel caso;
         * - niente `MATCH_DEFAULT_ONLY`, che esclude chi non dichiara `CATEGORY_DEFAULT`: quella
         *   categoria conta per la risoluzione implicita, e [open] parte con un componente
         *   esplicito.
         * Un elenco incompleto non dà errori: il selettore mostra solo l'editor interno, come su
         * un telefono senza editor.
         */
        val found = ask(pm, Intent.ACTION_EDIT, MIME)
        val editors = found.toChoices(context, pm, "")

        /*
         * ⚠️ **Chi non si dichiara editor si raggiunge condividendo**: un'app senza filtro
         * `ACTION_EDIT` (come Magic Eraser) nessuna domanda di modifica la troverà mai.
         * ⚠️ **Si toglie chi accetta anche il testo**: chi riceve un'immagine è anche la
         * messaggistica, la posta, il cloud, il Bluetooth, decine di voci. Un'app che tratta
         * immagini accetta immagini e non testo semplice; una che manda contenuti accetta tutti e
         * due. È un'euristica: può lasciare fuori un editor che accetta il testo e tenere dentro
         * un archivio in rete, per questo nel selettore le due famiglie restano separate.
         * Si scarta prima di chiedere nomi e icone, che costano (rilievo B13 della revisione
         * Setaccio).
         */
        val talkers = ask(pm, Intent.ACTION_SEND, TEXT)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
        val known = editors.map { it.id }.toSet()
        val others = ask(pm, Intent.ACTION_SEND, MIME)
            .filterNot { info ->
                val act = info.activityInfo ?: return@filterNot true
                act.packageName in talkers ||
                    ComponentName(act.packageName, act.name).flattenToString() in known
            }
            .toChoices(context, pm, SHARED)
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
        // AIV stessa non compare fra le scelte: aprirebbe sé stessa.
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
     * Se l'app scelta è stata disinstallata, la voce nelle impostazioni torna a dire 'nessuna'
     * e il menu torna a chiedere.
     */
    fun labelOf(context: Context, id: String): String? {
        if (id.isBlank()) return null
        if (isInternal(id)) return context.getString(R.string.editor_internal)
        // Si chiede il solo componente salvato, senza rifare l'elenco (rilievo B13). Un'app
        // disinstallata risponde con un'eccezione, cioè `null`.
        val component = ComponentName.unflattenFromString(id.removePrefix(SHARED)) ?: return null
        val pm = context.packageManager
        return runCatching { pm.getActivityInfo(component, 0).loadLabel(pm).toString() }.getOrNull()
    }

    /**
     * Manda l'immagine all'app scelta.
     *
     * ⚠️ **Il permesso di scrittura si concede insieme all'indirizzo**: senza
     * `FLAG_GRANT_WRITE_URI_PERMISSION` l'editor apre l'immagine e poi non riesce a salvarla,
     * con un errore suo da cui non si risale ad AIV.
     * Il componente è esplicito: con un intent generico Android chiederebbe di scegliere ogni
     * volta.
     */
    fun open(context: Context, uri: Uri, id: String): Boolean {
        val shared = id.startsWith(SHARED)
        val component = ComponentName.unflattenFromString(id.removePrefix(SHARED)) ?: return false
        /*
         * Con `ACTION_EDIT` l'app riceve l'immagine e la riscrive dov'era; con `ACTION_SEND` ne
         * riceve una copia e salva dove decide lei, e l'originale non è toccato. Per questo alla
         * condivisione si concede la sola lettura.
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
        // Il falso copre ogni modo di non partire: a chi chiama basta sapere se avvisare.
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }

    /** Il tipo che si chiede e si dichiara, scritto in un posto solo. */
    private const val MIME = "image/*"

    /** Il tipo che serve solo a escludere: chi accetta anche questo non è un'app per immagini. */
    private const val TEXT = "text/plain"

    /**
     * Due indirizzi finti, per la sola domanda al sistema.
     *
     * Non si aprono mai e non devono esistere: servono a far combaciare i filtri che pretendono
     * uno schema. La domanda è quale app, non che cosa c'è all'indirizzo.
     */
    private val SAMPLE_CONTENT: Uri = Uri.parse("content://media/external/images/media/1")
    private val SAMPLE_FILE: Uri = Uri.parse("file:///storage/emulated/0/Pictures/x.jpg")
}

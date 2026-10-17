package io.github.roccobot.aiv

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import org.junit.rules.ExternalResource

/**
 * I modelli dell'app costruiti dentro una prova, e chiusi alla sua fine (6.30, rilievo G16 della
 * revisione Setaccio).
 *
 * ⚠️⚠️ **Un `ViewerViewModel` costruito a mano non si chiude mai**: senza un `ViewModelStore` nessuno
 * chiama `onCleared`, e il suo `viewModelScope` resta vivo per il resto della corsa, coi flussi che
 * non finiscono (impostazioni, recenti, l'importazione di un file di impostazioni). Un'importazione
 * fatta da un'altra classe più avanti li risveglia tutti, e rileggono il disco mentre gira un'altra
 * prova: un errore che non cade nella prova che l'ha causato.
 *
 * Si usa come regola: `@get:Rule val modelli = ModelliChiusi()`, poi `modelli.nuovo(app)`.
 * `ModelliChiusiTest` controlla che nessuna prova costruisca più il modello a mano.
 */
class ModelliChiusi : ExternalResource() {

    private val stores = mutableListOf<ViewModelStore>()

    /** Un modello nuovo, in un archivio suo: ogni chiamata dà un modello distinto. */
    fun nuovo(app: Application): ViewerViewModel {
        val store = ViewModelStore().also { stores += it }
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        return ViewModelProvider(store, factory)[ViewerViewModel::class.java]
    }

    override fun after() {
        stores.forEach { it.clear() }
        stores.clear()
    }
}

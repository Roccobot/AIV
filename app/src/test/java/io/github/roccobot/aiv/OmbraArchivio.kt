package io.github.roccobot.aiv

import android.os.Environment
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * L'unica cosa che impedisce al banco di montare la **schermata vera** delle cartelle.
 *
 * ⚠️⚠️ **`Environment.isExternalStorageManager()` NON è coperto da Robolectric**, e non va in
 * errore in modo leggibile: muore con un `ArrayIndexOutOfBoundsException` dentro il metodo del
 * sistema, che si legge come un difetto dell'app e non come uno strumento che manca. Da lì passa
 * `Folder.granted`, che la schermata iniziale chiama alla prima composizione.
 *
 * ⚠️ **Risponde `false`, cioè 'permesso non concesso'**, che è lo stato in cui una macchina
 * senza telefono si trova davvero: la schermata mostra l'invito a concedere l'accesso, e il FAB
 * c'è lo stesso, perché non dipende dal permesso.
 */
@Implements(Environment::class)
class OmbraArchivio {
    companion object {
        @JvmStatic
        @Implementation
        fun isExternalStorageManager(): Boolean = false
    }
}

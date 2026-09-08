package io.github.roccobot.aiv

import android.os.Environment
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Come [OmbraArchivio], ma risponde **'permesso concesso'**.
 *
 * ⚠️⚠️ **NE SERVONO DUE, E NON È UN DOPPIONE**: le due risposte compongono due schermate diverse,
 * e ognuna serve a una classe di prove. Con `false`, che è la verità di una macchina senza
 * telefono, la schermata iniziale mostra l'invito a concedere l'accesso, e là si misura quello che
 * non dipende dai dati (il FAB, l'albero semantico). Con `true` la stessa schermata compone
 * l'**elenco delle cartelle**, che è l'unica scena in cui esiste qualcosa da scorrere.
 *
 * ⚠️ **Il metodo è lo stesso e la ragione per cui va coperto pure**: `isExternalStorageManager()`
 * non è coperto da Robolectric e muore con un `ArrayIndexOutOfBoundsException` dentro il metodo di
 * sistema, cioè con un errore che si legge come un difetto dell'app. Il perché per esteso vive su
 * [OmbraArchivio].
 */
@Implements(Environment::class)
class ArchivioAperto {
    companion object {
        @JvmStatic
        @Implementation
        fun isExternalStorageManager(): Boolean = true
    }
}

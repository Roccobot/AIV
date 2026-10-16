package io.github.roccobot.aiv

import android.os.Environment
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Resetter

/**
 * L'unica cosa che impedisce al banco di montare la **schermata vera** delle cartelle.
 *
 * ⚠️⚠️ **`Environment.isExternalStorageManager()` NON è coperto da Robolectric**, e non va in
 * errore in modo leggibile: muore con un `ArrayIndexOutOfBoundsException` dentro il metodo del
 * sistema, che si legge come un difetto dell'app e non come uno strumento che manca. Da lì passa
 * `Folder.granted`, che la schermata iniziale chiama alla prima composizione.
 *
 * ⚠️ **Di serie risponde `false`, cioè 'permesso non concesso'** ([granted]), che è lo stato in
 * cui una macchina senza telefono si trova davvero: la schermata mostra l'invito a concedere
 * l'accesso, e là si misura quello che non dipende dai dati (il FAB, l'albero semantico). Una
 * prova che vuole l'**elenco delle cartelle**, l'unica scena in cui esiste qualcosa da scorrere,
 * accende [granted] prima di montare la schermata; fra una prova e l'altra lo spegne [reset].
 * Un'ombra sola con uno stato e non due ombre che differiscono per un `true` (scelta I1 sulla
 * voce G8 della revisione Setaccio).
 * ⚠️⚠️ **PRENDE IL POSTO DELL'OMBRA DI ROBOLECTRIC PER `Environment`, E NON LE SI AGGIUNGE**
 * (misurato il 2026-09-25): chi la dichiara perde le altre implementazioni di quella classe,
 * quindi `getExternalFilesDir` gira sul metodo di sistema e muore dentro `Environment`. Il
 * cestino vive in quella cartella, ed è la ragione per cui `CestinoSpazioTest` non la dichiara.
 * Chi volesse le due cose insieme la faccia estendere `ShadowEnvironment`, sapendo che cambia
 * l'ambiente di tutte le prove che la usano.
 */
@Implements(Environment::class)
class OmbraArchivio {
    companion object {
        /**
         * La risposta di [isExternalStorageManager]: `true` vuol dire permesso concesso.
         *
         * Si accende nella prova o nel suo `@Before`, prima di montare la schermata, che la legge
         * alla prima composizione.
         */
        var granted = false

        @JvmStatic
        @Implementation
        fun isExternalStorageManager(): Boolean = granted

        /** Robolectric la chiama dopo ogni prova: la seguente parte di nuovo senza permesso. */
        @JvmStatic
        @Resetter
        fun reset() {
            granted = false
        }
    }
}

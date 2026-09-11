package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.RuntimeShader
import android.graphics.Shader.TileMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Il banco del **programma dello shader**, cioè il pezzo che l'editor completo fa girare su ogni
 * pixel dell'immagine.
 *
 * ⚠️⚠️ **NASCE DAL DIFETTO DELLA `2.14`, CHE È ARRIVATO A LUI INTERO**: nessuno dei cinque
 * cursori muoveva l'immagine, e il salvataggio rispondeva che il telefono non ce l'aveva fatta.
 * La causa era una parola: `out` è un **qualificatore di parametro** del linguaggio, quindi
 * `half3 out = ...` è un errore di sintassi e il programma intero non compilava. Su ogni telefono,
 * sempre, fin dal primo cursore.
 *
 * ⚠️⚠️ **E A NASCONDERLO È STATA LA RETE CHE DOVEVA PROTEGGERE**: `lookShader` avvolge la
 * compilazione in un `runCatching` perché un programma rifiutato non faccia cadere l'app mentre
 * disegna un fotogramma. Quella riga serve e resta, ma trasforma un errore di sintassi in un
 * `null`, cioè in 'questo telefono non sa farlo'. Nessun controllo sul **testo** del programma
 * poteva vederlo: Kotlin compila una stringa senza guardarci dentro, esattamente come `aapt2` non
 * guarda dentro un `pathData`.
 *
 * ⚠️⚠️ **QUINDI QUI IL PROGRAMMA SI COMPILA SENZA RETE**, ed è tutto il valore di questa classe:
 * un errore di sintassi arriva col messaggio del compilatore, la riga e la colonna, invece che
 * come un `null` da interpretare.
 *
 * ⚠️⚠️ **MA I PIXEL CHE NE ESCONO IL BANCO NON LI PUÒ VEDERE, ed è misurato**: disegnare con
 * questo programma su una tela di memoria fallisce con *Software rendering doesn't support
 * RuntimeShader*, perché Robolectric disegna col processore e Android quella strada la vieta.
 * Quindi che il contrasto contrasti e che le ombre sollevino gli scuri **si guarda sul telefono**,
 * e la voce di collaudo lo chiede. Scriverne una versione in Kotlin per poterla misurare qui
 * sarebbe la seconda copia della stessa matematica, cioè la cosa che `Adjust.kt` esiste per non
 * avere.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContoTest {

    /**
     * Il programma compila.
     *
     * ⚠️ **Senza `runCatching`, di proposito**: è la riga che nella `2.14` avrebbe fermato il
     * rilascio, e che invece non esisteva.
     */
    @Test
    fun `il programma compila`() {
        RuntimeShader(LIGHT_AGSL)
    }

    /**
     * E `lookShader` lo consegna davvero, coi cinque valori dentro.
     *
     * ⚠️ **Misura la forma esatta del difetto arrivato a lui**: se il programma non compila, o se
     * un nome di uniform non combacia, questa funzione risponde `null` e l'app mostra l'immagine
     * senza il conto applicato, cioè un cursore che non fa niente.
     */
    @Test
    fun `lookShader consegna il programma coi cinque valori`() {
        val pieno = Look(
            Light(exposure = 1f, brightness = 0.3f, contrast = -0.4f, shadows = 0.5f, highlights = -0.5f)
        )
        assertNotNull(
            "lookShader ha risposto null: il programma non compila o un uniform non combacia",
            lookShader(sorgente(), pieno)
        )
    }

    /** Una sorgente qualunque: qui conta che il programma la accetti, non che colore abbia. */
    private fun sorgente(): BitmapShader {
        val mappa = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        mappa.eraseColor(Color.rgb(128, 128, 128))
        return BitmapShader(mappa, TileMode.CLAMP, TileMode.CLAMP)
    }
}

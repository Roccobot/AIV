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
        RuntimeShader(LOOK_AGSL)
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
            Light(
                exposure = 1f, contrast = -0.4f, highlights = -0.5f, shadows = 0.5f,
                whites = 0.3f, blacks = -0.2f
            )
        )
        assertNotNull(
            "lookShader ha risposto null: il programma non compila o un uniform non combacia",
            lookShader(sorgente(), pieno, SPAN)
        )
    }

    /**
     * E lo consegna anche con le otto fasce dell'HSL dentro.
     *
     * ⚠️⚠️ **QUESTI UNIFORM SONO ARRAY, CHE È UNA FORMA CHE IL PROGRAMMA NON AVEVA**: sei array da
     * otto valori (i centri, i due raggi e i tre cursori per fascia), consegnati con la variante di
     * `setFloatUniform` che prende un `FloatArray`. Un array di lunghezza sbagliata o un nome che
     * non combacia non dà errore di compilazione: dà un'eccezione al primo fotogramma, che la rete
     * di `lookShader` trasformerebbe in un `null`, cioè in 'questo telefono non sa farlo'. È
     * esattamente la forma del difetto della `2.14`.
     */
    @Test
    fun `lookShader consegna il programma con le otto fasce`() {
        val fasce = Mix.NONE
            .swap(0) { it.copy(hue = 0.5f, sat = -0.3f, lum = 0.8f) }
            .swap(5) { it.copy(lum = -1f) }
        assertNotNull(
            "lookShader ha risposto null: un array di uniform non combacia",
            lookShader(sorgente(), Look(mix = fasce), SPAN)
        )
    }

    /**
     * E lo consegna anche col Dettaglio dentro, che porta due uniform di una forma nuova.
     *
     * ⚠️⚠️ **I DUE PASSI DEL VICINATO SONO `float2` E NON `half`, ED È LA FORMA CHE NON C'ERA**:
     * si sommano alle coordinate del pixel, che nel salvataggio arrivano a duemila, e in `half`
     * un numero così grande perde i decimali. Un nome che non combacia o un tipo sbagliato non dà
     * errore di compilazione: dà un'eccezione al primo fotogramma, che la rete di `lookShader`
     * trasformerebbe in un `null`, cioè in 'questo telefono non sa farlo'. È la stessa forma del
     * difetto della `2.14`.
     */
    @Test
    fun `lookShader consegna il programma col Dettaglio`() {
        val fine = Detail(
            sharpen = 0.7f, radius = 0.5f, masking = 0.4f, noise = 0.6f, noiseColor = 0.3f
        )
        assertNotNull(
            "lookShader ha risposto null: un uniform del Dettaglio non combacia",
            lookShader(sorgente(), Look(detail = fine), SPAN)
        )
    }

    /** Una sorgente qualunque: qui conta che il programma la accetti, non che colore abbia. */
    private fun sorgente(): BitmapShader {
        val mappa = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        mappa.eraseColor(Color.rgb(128, 128, 128))
        return BitmapShader(mappa, TileMode.CLAMP, TileMode.CLAMP)
    }

    private companion object {
        /**
         * Il lato lungo che si dichiara al programma: qui conta solo che ci sia, perché il
         * vicinato del Dettaglio si ricava da lui e non da quanto è grande la sorgente finta.
         */
        const val SPAN = 4000f
    }
}

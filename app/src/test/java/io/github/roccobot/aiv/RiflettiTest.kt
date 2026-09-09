package io.github.roccobot.aiv

import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Il banco di prova della **riflessione**: la posa, il rettangolo e l'orientamento EXIF.
 *
 * ⚠️⚠️ **NASCE CON LA FUNZIONE E NON DOPO UN DIFETTO**, ed è il caso proattivo di
 * `AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e quando no': qui il codice può essere
 * valido e sbagliato in silenzio, perché uno specchio davanti a una rotazione la **rovescia**
 * (`M ∘ R(k) = R(-k) ∘ M`). Chi scrivesse una somma al posto della sottrazione otterrebbe
 * un'immagine girata dalla parte opposta **solo** quando c'è già una rotazione: a immagine
 * dritta tutte le prove passerebbero, e il difetto arriverebbe a lui.
 *
 * ⚠️ **Quello che il banco non vede**: i pixel veri, cioè che [spunBy] e la matrice di Android
 * girino davvero l'immagine come dice la posa, e che un JPEG riflesso senza perdita si riapra
 * dritto. Quello si guarda sul telefono, e la voce di collaudo lo chiede.
 */
@RunWith(AndroidJUnit4::class)
class RiflettiTest {

    /** Le otto pose, che sono tutte quelle in cui si può mettere un'immagine. */
    private val pose = (0..3).flatMap { t -> listOf(Spin(t, false), Spin(t, true)) }

    /**
     * **Caso 1: uno specchio rovescia i quarti di giro accumulati.**
     *
     * ⚠️ È il conto che questa prova esiste per presidiare: girato di un quarto e poi
     * riflesso, il risultato è 'riflesso e girato di **tre**', non di uno.
     */
    @Test
    fun `lo specchio rovescia la rotazione`() {
        assertEquals(Spin(3, true), Spin(1, false).then(Spin.ACROSS))
        assertEquals(Spin(1, true), Spin(3, false).then(Spin.ACROSS))
        // ⚠️ La controprova del segno: con una somma verrebbe Spin(1, true), che è la posa
        // opposta e su una fotografia simmetrica non si distingue.
        assertNotEquals(Spin(1, true), Spin(1, false).then(Spin.ACROSS))
    }

    /**
     * **Caso 2: due specchi uguali si annullano, e quattro quarti di giro anche.**
     *
     * ⚠️ Vale da qualunque posa, non solo da quella dritta: è la proprietà che rende il
     * tasto un gesto che si può disfare toccandolo di nuovo.
     */
    @Test
    fun `due specchi uguali tornano al punto di partenza`() {
        for (posa in pose) {
            assertEquals(posa, posa.then(Spin.ACROSS).then(Spin.ACROSS))
            assertEquals(posa, posa.then(Spin.DOWN).then(Spin.DOWN))
            var giro = posa
            repeat(4) { giro = giro.then(Spin(1, false)) }
            assertEquals(posa, giro)
        }
    }

    /**
     * **Caso 3: il verticale è l'orizzontale più mezzo giro.**
     *
     * ⚠️ È la definizione di [Spin.DOWN], e provarla qui vuol dire che il tocco lungo non ha
     * un meccanismo suo da tenere allineato: se un domani qualcuno lo scrivesse a parte, questa
     * riga direbbe subito che le due strade divergono.
     */
    @Test
    fun `il verticale e l'orizzontale piu mezzo giro`() {
        for (posa in pose) {
            assertEquals(
                posa.then(Spin.ACROSS).then(Spin(2, false)),
                posa.then(Spin.DOWN)
            )
        }
    }

    /**
     * **Caso 4: l'orientamento EXIF segue la composizione delle pose.**
     *
     * ⚠️⚠️ **È LA PROVA CHE LEGA I DUE MONDI, e non ricopia nessuna tabella**: il salvataggio
     * senza perdita cambia il solo tag EXIF, quindi due gesti fatti uno dopo l'altro devono
     * dare lo stesso tag della loro posa composta. Se `ImageEdit.spun` e `Spin.then` non
     * dicessero la stessa cosa, un JPEG riflesso e poi girato si riaprirebbe in una posa che
     * l'editor non ha mai mostrato.
     */
    @Test
    fun `l'EXIF composto e quello della posa composta`() {
        val partenze = listOf(
            ExifInterface.ORIENTATION_NORMAL,
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_FLIP_VERTICAL,
            ExifInterface.ORIENTATION_TRANSPOSE
        )
        for (da in partenze) {
            for (prima in pose) {
                for (poi in pose) {
                    val passo = ImageEdit.spun(
                        ImageEdit.spun(da, prima.turns, prima.mirror),
                        poi.turns,
                        poi.mirror
                    )
                    val insieme = prima.then(poi)
                    assertEquals(
                        "da $da con $prima poi $poi",
                        ImageEdit.spun(da, insieme.turns, insieme.mirror),
                        passo
                    )
                }
            }
        }
    }

    /**
     * **Caso 5: il rettangolo di ritaglio si muove con l'immagine.**
     *
     * ⚠️ Uno specchio orizzontale ribalta la x: quello che era il quinto sinistro diventa il
     * quinto destro. ⚠️ **E il verticale lascia la x dov'è**, che è la differenza che un segno
     * sbagliato farebbe sparire.
     */
    @Test
    fun `il rettangolo si specchia con l'immagine`() {
        val angolo = ImageEdit.Crop(0f, 0f, 0.2f, 0.5f)

        val orizzontale = spunRect(angolo, Spin.ACROSS)
        assertEquals(0.8f, orizzontale.left, 1e-6f)
        assertEquals(1f, orizzontale.right, 1e-6f)
        assertEquals(0f, orizzontale.top, 1e-6f)
        assertEquals(0.5f, orizzontale.bottom, 1e-6f)

        val verticale = spunRect(angolo, Spin.DOWN)
        assertEquals(0f, verticale.left, 1e-6f)
        assertEquals(0.2f, verticale.right, 1e-6f)
        assertEquals(0.5f, verticale.top, 1e-6f)
        assertEquals(1f, verticale.bottom, 1e-6f)
    }

    /**
     * **Caso 6: chi aggiorna trova 'Rifletti' in mezzo e non in fondo.**
     *
     * ⚠️⚠️ **È IL CASO CHE NESSUNO GUARDA**: sul telefono di chi ha già usato l'app l'ordine
     * della fila è salvato con quattro gettoni, e il tasto nuovo non c'è. `padOrderOf` lo
     * infila dove lo mette [TURN_KEYS], cioè fra 'Centra in orizzontale' e 'Ruota a sinistra',
     * che è il posto che ha chiesto lui. Senza quel meccanismo comparirebbe in coda, cioè
     * dopo le due rotazioni.
     */
    @Test
    fun `il tasto nuovo entra al suo posto in un ordine gia salvato`() {
        val vecchio = listOf(
            PadKey.CENTRE_DOWN, PadKey.CENTRE_ACROSS, PadKey.TURN_LEFT, PadKey.TURN_RIGHT
        ).map { it.token }
        assertEquals(TURN_KEYS, padOrderOf(vecchio, TURN_KEYS))
        assertEquals(2, padOrderOf(vecchio, TURN_KEYS).indexOf(PadKey.FLIP))
    }
}

package io.github.roccobot.aiv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La salita di un pannello a tastiera aperta, e il tetto che gli impedisce di finire sul notch.
 *
 * ⚠️⚠️ **NASCE COL DIFETTO CHE È ARRIVATO A LUI** (riscontro del giro della `1.80`, campo libero
 * punto B: *Controlla la posizione della finestra di dialogo per la modifica dell'estensione: si
 * apre talmente in alto da finire sul notch e quasi sull'orologio di sistema*), come prescrive
 * `AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e quando no'.
 * ⚠️⚠️ **PROVA LA FUNZIONE E NON LA SCHERMATA, E LA RAGIONE È MISURATA**: il ramo che sale gira
 * solo con la tastiera in scena, e in Robolectric `getRootWindowInsets` non riporta nessuna
 * tastiera, quindi una prova che aprisse il pannellino non eserciterebbe affatto quel conto.
 * Passerebbe in verde senza guardare niente, che è il modo in cui una prova mente.
 */
class SalitaTest {

    /**
     * I numeri sono quelli di un telefono con il notch e la tastiera aperta: schermo alto 2400,
     * barra di stato 100, tastiera più barra di navigazione 1000, quindi la finestra del dialogo
     * è alta 1300. Il pannellino dell'estensione è corto: 400.
     */
    private val schermo = 2400
    private val finestra = 1300
    private val pannello = 400
    private val aria = 48

    @Test
    fun `la salita ricavata dallo schermo intero manderebbe il pannello fuori dalla finestra`() {
        // ⚠️ È il conto che c'era fino alla `1.80`: la salita viene dall'altezza dello schermo
        // meno le barre, ma a centrare la scatola gonfia è la finestra del dialogo.
        val room = (schermo - pannello) / 2 - aria
        val scatola = pannello + room * 2
        assertTrue(
            "senza tetto la scatola supera la finestra: $scatola contro $finestra",
            scatola > finestra
        )
        // ⚠️ E il pannello finisce **sopra** il bordo di sopra: y negativo vuol dire sul notch.
        val y = (finestra - scatola) / 2
        assertTrue("il pannello uscirebbe in alto di ${-y} pixel", y < 0)
    }

    @Test
    fun `col tetto il pannello resta dentro la finestra, con la sua aria sopra`() {
        val room = (schermo - pannello) / 2 - aria
        val climb = climbFor(room = room, panel = pannello, box = finestra, air = aria)
        val scatola = pannello + climb * 2
        val y = (finestra - scatola) / 2
        assertTrue("la scatola non supera la finestra", scatola <= finestra)
        assertTrue("resta almeno l'aria sopra il pannello: $y", y >= aria)
    }

    @Test
    fun `quando la finestra dice il vero la salita non si tocca`() {
        // ⚠️ Il caso sano: la misura dello schermo e quella del contenitore coincidono, e allora
        // il tetto non deve limitare niente, o toglierebbe la deroga che lui ha chiesto.
        val room = (finestra - pannello) / 2 - aria
        assertEquals(
            room,
            climbFor(room = room, panel = pannello, box = finestra, air = aria)
        )
    }

    @Test
    fun `un contenitore senza vincolo non limita la salita`() {
        val room = 300
        assertEquals(room, climbFor(room = room, panel = pannello, box = 0, air = aria))
    }

    @Test
    fun `un pannello piu alto della finestra non fa salire di niente`() {
        val alto = finestra + 200
        assertEquals(0, climbFor(room = 500, panel = alto, box = finestra, air = aria))
    }
}

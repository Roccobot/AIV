package io.github.roccobot.aiv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Una finestra in cui si scrive va in alto, e non si muove mentre la tastiera cambia misura.
 *
 * ⚠️⚠️ **NASCE COL DIFETTO CHE È ARRIVATO A LUI** (riscontro del giro della `1.82`, voce
 * `rinomina-ferma` non approvata: *secondo terzo carattere inserito o cancellato la finestra si
 * sposta da troppo in basso a molto in alto, e poi ogni 3/4 caratteri c'è un flash della stessa
 * finestra in posizione molto più ribassata*), come prescrive `AIV/CLAUDE.md` § '🧪 Quando si
 * scrive una prova, e quando no'.
 * ⚠️⚠️ **PROVA LA FUNZIONE E NON LA SCHERMATA, E LA RAGIONE È MISURATA**: quello che deve reggere
 * è che il pannello non si sposti **mentre la finestra cambia**, e in Robolectric
 * `getRootWindowInsets` non riporta nessuna tastiera, quindi una prova che aprisse la rinomina
 * non vedrebbe mai la misura cambiare. Passerebbe in verde senza guardare niente, che è il modo
 * in cui una prova mente.
 * ⚠️ **Sostituisce `SalitaTest`, che misurava la deroga della `1.82`**: quel conto non esiste
 * più, e una prova che sopravvive alla funzione che provava è una prova che ricopia il codice.
 */
class AltoTest {

    /**
     * I numeri sono quelli di un telefono con il notch: schermo alto 2400, barra di stato 100,
     * barra di navigazione 100. La finestra a tastiera chiusa è 2200; con la tastiera aperta
     * scende a 1300, e con la barra dei suggerimenti in più a 1220.
     */
    private val chiusa = 2200
    private val aperta = 1300
    private val suggerimenti = 1220
    private val pannello = 400
    private val aria = 70

    /** Dove comincia il pannello dentro la finestra, che è il conto che la finestra stessa fa. */
    private fun top(box: Int, panel: Int): Int {
        val climb = pinClimb(box = box, panel = panel, air = aria)
        return (box - (panel + climb * 2)) / 2
    }

    @Test
    fun `il pannello comincia all'aria dichiarata, qualunque sia la finestra`() {
        assertEquals(aria, top(chiusa, pannello))
        assertEquals(aria, top(aperta, pannello))
        assertEquals(aria, top(suggerimenti, pannello))
    }

    @Test
    fun `una tastiera che cambia misura non sposta il pannello di un pixel`() {
        // ⚠️ È la forma esatta del difetto: fra questi tre numeri passa tutto quello che succede
        // mentre si scrive (la tastiera che si apre, la barra dei suggerimenti che compare e
        // sparisce), e il pannello deve stare fermo.
        val posti = listOf(chiusa, aperta, suggerimenti).map { top(it, pannello) }
        assertEquals(
            "il pannello si è spostato al variare della finestra: $posti",
            1,
            posti.distinct().size
        )
    }

    @Test
    fun `nemmeno un pannello che cresce sposta il proprio bordo di sopra`() {
        // ⚠️ Il campo che va a capo, o un comando che compare: il pannello si allunga in giù, e
        // il testo in cima resta dov'era.
        assertEquals(aria, top(aperta, pannello))
        assertEquals(aria, top(aperta, pannello + 120))
    }

    @Test
    fun `un pannello che riempie la finestra non si alza affatto`() {
        val alto = aperta - aria
        assertEquals(0, pinClimb(box = aperta, panel = alto, air = aria))
        // ⚠️ Là il pannello è già più alto dell'aria concessa, quindi la finestra lo centra: è il
        // caso in cui non c'è niente da guadagnare alzandolo.
        assertTrue(top(aperta, alto) < aria)
    }

    @Test
    fun `un contenitore senza vincolo non alza niente`() {
        // ⚠️ Senza un `box` non esiste un bordo di sopra da cui misurare, e il chiamante ripiega
        // sulla finestra dello schermo: se manca anche quella, meglio un pannello centrato che
        // uno posato su un numero inventato.
        assertEquals(0, pinClimb(box = 0, panel = pannello, air = aria))
    }
}

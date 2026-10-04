package io.github.roccobot.aiv

import androidx.core.graphics.Insets
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * L'altezza in cui un pannello centrato si misura toglie le barre di sistema anche quando la
 * finestra del dialogo non le vede.
 *
 * ⚠️ **Nasce dall'allegato `popup_phoneH` del giro della `3.70`**: sul telefono in orizzontale la
 * conferma 'Vuoi nascondere...' arrivava a filo del fondo, sopra la barra dei gesti. I numeri sono
 * quelli dell'allegato: schermo alto 1200, barra di stato 110, barra dei gesti 40. Il pannello
 * cominciava a 187 e finiva a 1188, cioè centrato sull'area dentro le barre (635) e sceso di 52,
 * che è lo spostamento calcolato su un'altezza di 1200 invece che di 1050.
 * ⚠️ **Prova la funzione e non la schermata**, come `AltoTest`: in Robolectric un dialogo non ha
 * barre di sistema, e una prova che aprisse la conferma non vedrebbe niente.
 */
class CentroTest {

    private val barre = Insets.of(0, 110, 0, 40)

    @Test
    fun `il dialogo senza barre prende quelle dell'attivita`() {
        assertEquals(1050, insideBars(1200, own = Insets.NONE, host = barre))
    }

    @Test
    fun `le stesse barre viste due volte si tolgono una volta`() {
        assertEquals(1050, insideBars(1200, own = barre, host = barre))
    }

    @Test
    fun `la tastiera del dialogo vince sulla barra dei gesti`() {
        val tastiera = Insets.of(0, 0, 0, 500)
        assertEquals(1200 - 110 - 500, insideBars(1200, own = tastiera, host = barre))
    }
}

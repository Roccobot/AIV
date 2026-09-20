package io.github.roccobot.aiv

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **Tornare nell'app non butta via quello che si stava facendo**, dalla `2.78`.
 *
 * ⚠️⚠️ **IL DIFETTO È ARRIVATO A LUI, ED È IL PUNTO A DEL CAMPO LIBERO** (*quando l'editor è
 * aperto, se passo a un'altra app (senza chiudere AIV), poi torno, mi ritrovo l'editor chiuso e le
 * modifiche in corso perse*). L'app è `singleTop`, quindi toccando la sua icona mentre è già in
 * cima al proprio task Android consegna un `onNewIntent` con l'intento del launcher: un intento
 * che non porta nessun indirizzo, e che fino alla `2.77` riportava a casa.
 *
 * ⚠️ **Il banco può vederlo perché la decisione è nel modello**: `handleIntent` è una funzione che
 * si chiama senza montare niente, e quello che si misura è dove la schermata resta.
 *
 * ⚠️ **Che cosa NON misura**: che sul telefono il processo resti vivo. Se Android lo uccide in
 * secondo piano il modello muore con lui, e quella metà si guarda sul telefono, con la voce di
 * collaudo che lo dice.
 */
@RunWith(AndroidJUnit4::class)
class RitornoTest {

    private fun modello(): ViewerViewModel =
        ViewerViewModel(ApplicationProvider.getApplicationContext<Application>())

    /** L'avvio dall'icona: l'app parte da casa, cioè dall'elenco delle cartelle. */
    @Test
    fun `il primo intento senza indirizzo porta a casa`() {
        val model = modello()
        model.handleIntent(Intent(Intent.ACTION_MAIN), fresh = true)
        assertEquals(Screen.Folders(forStart = false), model.screen)
    }

    /**
     * **Il caso suo**: si sta lavorando da qualche parte, si torna dall'icona, e la schermata
     * resta quella.
     *
     * ⚠️ **La schermata di prova non è l'editor** e la misura vale lo stesso: quel ramo non guarda
     * dove si è, guarda **se l'intento è il primo**. Montare l'editor vorrebbe dire un file vero
     * da decodificare per misurare una condizione che non lo tocca.
     */
    @Test
    fun `un intento senza indirizzo a giro iniziato non tocca la schermata`() {
        val model = modello()
        model.handleIntent(Intent(Intent.ACTION_MAIN), fresh = true)
        model.chooseStartFolder()
        val dove = model.screen
        model.handleIntent(Intent(Intent.ACTION_MAIN), fresh = false)
        assertEquals(dove, model.screen)
        // ⚠️ La controprova vive qui dentro: con la lettura di prima, cioè `fresh`, la stessa
        // chiamata riporta a casa. Senza, questa prova resterebbe verde anche col difetto.
        model.handleIntent(Intent(Intent.ACTION_MAIN), fresh = true)
        assertEquals(Screen.Folders(forStart = false), model.screen)
    }

    /**
     * **Chi chiede un'immagine viene servito lo stesso**, anche a giro iniziato.
     *
     * ⚠️ È la ragione per cui la guardia non guarda solo l'indirizzo: un `GET_CONTENT` non ne
     * porta nessuno ma chiede qualcosa, cioè di scegliere, e quella richiesta deve arrivare a
     * casa dove ci sono le immagini.
     */
    @Test
    fun `una richiesta di scelta arriva anche a giro iniziato`() {
        val model = modello()
        model.handleIntent(Intent(Intent.ACTION_MAIN), fresh = true)
        model.chooseStartFolder()
        model.handleIntent(Intent(Intent.ACTION_GET_CONTENT), fresh = false)
        assertEquals(Screen.Folders(forStart = false), model.screen)
        assertEquals(true, model.picking)
    }
}

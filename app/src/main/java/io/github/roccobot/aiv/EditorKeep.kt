package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

/**
 * Il lavoro dell'editor completo, che deve sopravvivere a una ricreazione dell'attività.
 *
 * ⚠️⚠️ **VIVE IN UN `ViewModel` DALLA `6.27`** (rilievo A9 della revisione della 6.25): con
 * `remember` la storia, l'indice e quello che si vede finivano con l'attività, che Android rifà
 * ruotando il tablet largo, cambiando il tema di sistema (anche quello automatico della sera), la
 * lingua o la dimensione dei caratteri, ed entrando in multi-finestra. L'editor si riapriva
 * sull'immagine intonsa, senza la domanda 'Vuoi scartare le modifiche?', perché non era un'uscita.
 * ⚠️ **Si svuota quando l'editor si chiude davvero** ([close], chiamata da chi lo mostra quando
 * esce di scena e l'attività non sta cambiando configurazione), o riaprendo la stessa immagine si
 * ritroverebbe il lavoro scartato. **Un'immagine diversa riparte da zero** ([open]).
 * ⚠️ **La morte del processo resta fuori**: servirebbe scrivere un `Look` su disco, e le correzioni
 * hanno dei pixel. È il secondo passo che la revisione lascia da decidere.
 *
 * Rocco Casadei, a.k.a. Roccobot
 */
internal class EditorKeep : ViewModel() {

    /** L'immagine a cui appartiene il lavoro tenuto, o `null` quando non ce n'è. */
    private var uri: Uri? = null

    /** La storia dei valori: vedi la nota in `AdvancedEditorScreen`. */
    val history = mutableStateOf(listOf(Look.NONE))

    /** Dove si è dentro [history]. */
    val at = mutableIntStateOf(0)

    /** Quello che si vede adesso, compreso il movimento di cursore ancora in corso. */
    val look = mutableStateOf(Look.NONE)

    /** Tiene il lavoro di [next]: se prima ce n'era un altro, lo butta. */
    fun open(next: Uri) {
        if (uri == next) return
        uri = next
        reset()
    }

    /** L'editor è uscito di scena davvero: il lavoro non serve più. */
    fun close() {
        uri = null
        reset()
    }

    private fun reset() {
        history.value = listOf(Look.NONE)
        at.intValue = 0
        look.value = Look.NONE
    }
}

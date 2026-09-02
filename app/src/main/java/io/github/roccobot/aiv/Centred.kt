package io.github.roccobot.aiv

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp

/**
 * Che cosa vuol dire 'centrato' in AIV, dalla 1.29.
 *
 * ⚠️⚠️ **CENTRATO IN ORIZZONTALE, E CENTRATO MA IL 15% PIÙ IN BASSO IN VERTICALE**
 * (definizione dell'utente, 2026-09-02: *da questo momento in AIV dire 'centrato' (su
 * elementi di UI di questo tipo) significa centrato in orizzontale + centrato, ma un 15% più
 * in basso, in verticale*). Non è un gusto: il pollice arriva più facilmente sotto la metà
 * dello schermo, e un dialogo esattamente al centro fa allungare la mano su un telefono
 * grande. Vale per **tutto** quello che si apre in mezzo: i dialoghi di conferma, i pannelli,
 * i modali, la scheda delle informazioni, i menu.
 *
 * ⚠️⚠️ **LA STRETTA È PARTE DELLA DEFINIZIONE, non una prudenza aggiunta**: *le cose
 * particolarmente alte si prendono lo spazio che serve*. Un dialogo alto quasi quanto lo
 * schermo, spinto giù del 15%, ne uscirebbe: qui lo spostamento si riduce da sé fino a
 * sparire, perché quello che scende non può superare l'aria che ha sotto.
 *
 * ⚠️ **Il 15% si misura sull'altezza della FINESTRA, non sullo spazio libero**: sullo spazio
 * libero sarebbe una frazione di una frazione, quindi su un dialogo alto il movimento
 * sparirebbe proprio dove il pollice fatica di più. La stretta interviene dopo, e solo se
 * serve.
 *
 * ⚠️⚠️ **NON ESISTE UN AGGANCIO GLOBALE PER I DIALOGHI IN COMPOSE**, e per questo il numero
 * sta qui e la riga si scrive a ogni chiamata: `AlertDialog` centra la sua superficie dentro
 * la propria finestra, e nessuna proprietà del dialogo sposta quel centro. Quello che si può
 * fare è avere **un** modificatore, che è quello che si è fatto: chi apre un dialogo nuovo lo
 * aggiunge, e il valore non è mai scritto due volte. La regola sta anche in `CLAUDE.md`,
 * perché un modificatore da ricordare senza una regola scritta prima o poi si dimentica.
 */
fun Modifier.lowered(): Modifier = layout { measurable, constraints ->
    val placed = measurable.measure(constraints)
    val free = (constraints.maxHeight - placed.height).coerceAtLeast(0)
    val air = LOWER_AIR.roundToPx()
    val room = (free / 2 - air).coerceAtLeast(0)
    val wanted = (constraints.maxHeight * LOWER_BY).toInt()
    val shift = minOf(wanted, room)
    /*
     * ⚠️⚠️ **LO SPOSTAMENTO STA DENTRO L'ALTEZZA RIPORTATA, e fino alla 1.33 NON c'era: è
     * il difetto che TAGLIAVA I DIALOGHI ALTI.** La nota di prima diceva l'opposto (misura
     * vera e movimento nel solo `place`, *così il genitore continua a centrare*), e la
     * ragione sembrava buona: un genitore che centra la scatola gonfia annulla metà del
     * movimento. Il guaio è che un figlio posato **fuori** dalla scatola dichiarata viene
     * **ritagliato**, e la finestra di un dialogo si dimensiona proprio su quella scatola:
     * il pannello scendeva del 15% e perdeva gli ultimi pixel, cioè la fila dei tasti.
     * Segnalato dall'utente con tre schermate (2026-09-02, voce `centro-15`): il dialogo
     * 'Info' senza la sua riga di comandi, e la rinomina tagliata a metà dei tasti.
     * ⚠️ **Il RADDOPPIO è quel conto, non una compensazione a occhio**: la finestra si
     * dimensiona sull'altezza dichiarata e la centra, quindi una scatola più alta di
     * `2*shift` sposta il contenuto vero di `shift`, che è la misura voluta. Chi togliesse
     * il `2` dimezzerebbe il movimento senza accorgersene, perché il difetto non si vede.
     * ⚠️ **E la stretta resta la stessa di prima**: `shift` non supera l'aria che c'è sotto
     * meno [LOWER_AIR], quindi su un dialogo alto quanto la finestra vale zero e la scatola
     * non si gonfia affatto.
     */
    layout(placed.width, placed.height + shift * 2) { placed.place(0, shift * 2) }
}

/**
 * Di quanto scende quello che è 'centrato', e quanta aria resta comunque sotto.
 *
 * ⚠️ **15% è la base scelta dall'utente**, e dalla 1.29 è **uno solo per tutta l'app**: il
 * menu contestuale della `1.28` scendeva del 17%, che era la metà di un intervallo indicato a
 * occhio. Due numeri per la stessa idea sono la stessa trappola degli angoli dei menu, e la
 * differenza fra 15 e 17 non la vede nessuno.
 */
const val LOWER_BY = 0.15f
val LOWER_AIR = 16.dp

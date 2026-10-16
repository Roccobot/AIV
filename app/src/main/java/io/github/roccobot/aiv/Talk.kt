package io.github.roccobot.aiv

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * I pezzi condivisi di quello che l'app **dice a un lettore di schermo**, e il bersaglio minimo
 * di un comando.
 *
 * Oltre alla voce di un comando, un lettore di schermo ha bisogno di sapere che cosa quel comando
 * è, se è scelto, se è un titolo e quanto è grande da toccare: questo file dichiara le ultime
 * quattro.
 *
 * Un file condiviso perché le stesse dichiarazioni servono in molte schermate e componenti, e
 * scritte a mano a ogni chiamata sarebbero altrettante occasioni di dimenticarne una (come
 * `lowered()` con la Sfocatura dietro i pannelli).
 *
 * L'albero semantico è un dato, quindi un'intestazione mancante, un ruolo sbagliato o un
 * bersaglio troppo piccolo si misurano sul banco: presidiato da `SemanticheTest`.
 */

/**
 * Il **bersaglio minimo** di un comando che si tocca, in altezza.
 *
 * Sono i 48dp delle linee guida di Material, e in casa servono ai comandi che non sono tasti:
 * un testo cliccabile a corpo `labelMedium` viene alto 16dp di riga più i suoi margini, cioè
 * poco più della metà.
 *
 * ⚠️ **`minimumInteractiveComponentSize()` di Material non allarga il bersaglio** (misurato sul
 * bytecode): `MinimumInteractiveModifierNode` è solo un `LayoutModifierNode`, quindi riserva lo
 * spazio e centra il contenuto, ma il bersaglio dei tocchi resta quello del nodo di layout più
 * interno. In `FilterKey` serve a tenere lontani due tasti vicini, e là il tocco è già 40dp per
 * la sua `size`.
 * Quindi conta l'ordine: il gesto si installa prima della misura, e così il suo riquadro è quello
 * allargato (per la stessa ragione il `clickable` precede il `padding`).
 */
internal val TAP_MIN = 48.dp

/**
 * Dichiara un **titolo di schermata** come intestazione, cioè come punto in cui un lettore di
 * schermo si fermi navigando per intestazioni.
 * Senza, per TalkBack un titolo è testo qualunque, e la navigazione per intestazioni non trova
 * niente.
 */
internal fun Modifier.heading(): Modifier = semantics { heading() }

/**
 * Riserva a un comando il [TAP_MIN] in altezza, centrandoci il contenuto.
 *
 * ⚠️ Va dopo il gesto nella catena, o non allarga il bersaglio: il perché è su [TAP_MIN].
 */
internal fun Modifier.tapRoom(): Modifier =
    sizeIn(minHeight = TAP_MIN).wrapContentHeight(Alignment.CenterVertically)

/**
 * Dichiara che le voci dentro sono una **scelta sola**, cioè che ne vale una per volta.
 *
 * Va sempre insieme a [picked] sulle voci: dice che il gruppo è una scelta e quanto è lungo, e
 * [picked] dice quale voce è scelta e che genere di voce è.
 */
internal fun Modifier.oneOf(): Modifier = selectableGroup()

/**
 * Dichiara una voce di una scelta esclusiva: il **ruolo** giusto e se è quella **scelta**.
 *
 * Il ruolo del chiamante vince su quello del componente (misurato dal banco): un `FilterChip` di
 * Material si dichiara `Role.Checkbox`, cioè una casella che si spunta da sola, mentre nell'app
 * le scelte sono a risposta unica. Con questa riga il ruolo è `RadioButton`.
 * ⚠️ `clearAndSetSemantics` cancellerebbe anche l'azione di tocco del componente, e la pastiglia
 * non si attiverebbe più col lettore di schermo.
 */
internal fun Modifier.picked(chosen: Boolean): Modifier = semantics {
    role = Role.RadioButton
    selected = chosen
}

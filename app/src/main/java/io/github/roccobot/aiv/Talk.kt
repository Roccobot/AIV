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
 * ⚠️⚠️ **NASCE DAL CENSIMENTO DELLA UI DEL 2026-09-05, che di rilievi di accessibilità ne ha
 * contati quindici**, cioè la famiglia più numerosa dopo i commenti invecchiati. Il fatto che li
 * tiene insieme è uno: l'app cura con precisione la **voce** di ogni comando (chi porta la
 * descrizione fra icona e testo è discusso in tre KDoc diversi) e tace su tutto il resto, cioè
 * su che **cosa** quel comando è, se è **scelto**, se è un **titolo** e quanto è **grande** da
 * toccare. Un lettore di schermo di quelle quattro cose ha bisogno quanto della prima.
 *
 * ⚠️ **Perché un file condiviso e non una riga per chiamante**: le stesse quattro dichiarazioni
 * servivano in sette schermate e in nove componenti, e scritte a mano sarebbero state sedici
 * occasioni di dimenticarne una. Il precedente in casa è `lowered()`, che porta con sé il velo
 * per la stessa ragione: chi chiama ottiene la cosa giusta, non due righe da ricordare.
 *
 * ⚠️⚠️ **E NON SI PUÒ PROVARE COL TELEFONO SPENTO, ma il banco vede tutto quello che serve**:
 * l'albero semantico è un dato, quindi un'intestazione mancante, un ruolo sbagliato o un
 * bersaglio troppo piccolo sono misure e non impressioni. È la ragione per cui questo lotto
 * porta `SemanticheTest`.
 */

/**
 * Il **bersaglio minimo** di un comando che si tocca, in altezza.
 *
 * Sono i 48dp delle linee guida di Material, e in casa servono ai comandi che non sono tasti:
 * un testo cliccabile a corpo `labelMedium` viene alto 16dp di riga più i suoi margini, cioè
 * poco più della metà.
 *
 * ⚠️⚠️ **`minimumInteractiveComponentSize()` DI MATERIAL NON FA QUESTO LAVORO, ed è misurato sul
 * bytecode**: `MinimumInteractiveModifierNode` implementa il solo `LayoutModifierNode`, quindi
 * **riserva lo spazio** e centra il contenuto, ma il bersaglio dei tocchi resta quello del nodo
 * di layout che sta **dentro** la catena. In `FilterKey` è usato per quello che sa fare, cioè
 * tenere lontani due tasti vicini, e là il tocco è già 40dp per la sua `size`.
 * - **Quindi la forma che funziona è l'ordine**: il gesto si installa **prima** della misura, e
 *   così il suo riquadro è quello allargato. È lo stesso fatto per cui in casa il `clickable`
 *   precede il `padding`, che è scritto in due KDoc del pannello delle impostazioni.
 */
internal val TAP_MIN = 48.dp

/**
 * Dichiara un **titolo di schermata** come intestazione, cioè come punto in cui un lettore di
 * schermo si fermi navigando per intestazioni.
 *
 * ⚠️ **Nessuno dei sette titoli dell'app lo dichiarava**, e per TalkBack erano testo qualunque:
 * la navigazione per intestazioni non trovava niente, quindi l'unico modo di arrivare in fondo
 * a una schermata era scorrere voce per voce.
 */
internal fun Modifier.heading(): Modifier = semantics { heading() }

/**
 * Riserva a un comando il [TAP_MIN] in altezza, centrandoci il contenuto.
 *
 * ⚠️ **Va DOPO il gesto nella catena**, o non serve a niente: il perché è su [TAP_MIN].
 */
internal fun Modifier.tapRoom(): Modifier =
    sizeIn(minHeight = TAP_MIN).wrapContentHeight(Alignment.CenterVertically)

/**
 * Dichiara che le voci dentro sono una **scelta sola**, cioè che ne vale una per volta.
 *
 * ⚠️ **Da sola non basta, e va sempre insieme a [picked] sulle voci**: dice che il gruppo è una
 * scelta e quanto è lungo, non quale voce sia scelta né che genere di voce sia.
 */
internal fun Modifier.oneOf(): Modifier = selectableGroup()

/**
 * Dichiara una voce di una scelta esclusiva: il **ruolo** giusto e se è quella **scelta**.
 *
 * ⚠️⚠️ **IL RUOLO DEL CHIAMANTE VINCE SU QUELLO DEL COMPONENTE, ed è misurato dal banco**: un
 * `FilterChip` di Material si scrive addosso `Role.Checkbox` (letto nel bytecode di `ChipKt`),
 * cioè si annuncia come una casella che si spunta da sola, mentre le sei chiamate dell'app sono
 * tutte scelte a risposta unica. Con questa riga il ruolo che arriva è `RadioButton`.
 * - ⚠️ **Non si usa `clearAndSetSemantics`**, che sarebbe l'altra via per riscrivere un ruolo:
 *   quello cancella anche l'azione di tocco del componente, cioè renderebbe la pastiglia
 *   inattivabile proprio per chi usa il lettore di schermo.
 */
internal fun Modifier.picked(chosen: Boolean): Modifier = semantics {
    role = Role.RadioButton
    selected = chosen
}

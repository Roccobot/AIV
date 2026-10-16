package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Il colore dell'**intestazione** di una cartella: le sedici tinte e la finestra che le mostra.
 *
 * Sedici colori preimpostati, come tondi in una griglia 4x4, per richiesta dell'utente.
 * Vive fuori dalle impostazioni perché è un dato di una cartella e non una preferenza dell'app:
 * si sceglie guardando quella cartella.
 */

/**
 * Una tinta di cartella: due colori, uno per tema, perché ognuno stacchi dal fondo del suo tema
 * (richiesta dell'utente). Nell'archivio resta la scelta, cioè un indice: col tema cambia solo
 * come quel colore si scrive.
 */
data class FrontTint(
    /** Il colore sul tema chiaro. */
    val light: Color,
    /** Il colore sul tema scuro. */
    val dark: Color
)

/**
 * Le sedici tinte, nell'ordine in cui compaiono nella griglia 4x4: la ruota intera, dal rosso
 * al rosa.
 *
 * Coprono tutta la ruota delle tonalità, su istruzione dell'utente (voce `tinte-coppie`).
 *
 * ⚠️ **Le tonalità sono a passo uniforme in OkLCh e non in HSL**, 22,5 gradi l'una dall'altra su
 * una ruota percettiva: lo stesso passo in HSL addensa i verdi e dirada i blu.
 * I bersagli di luminosità sono misurati sulla tavolozza che l'utente aveva approvato: 0,615 per
 * il tema chiaro e 0,760 per lo scuro. La croma ha un tetto di 0,17, o ogni tinta arriva al
 * limite del gamut e la tavolozza diventa fluorescente.
 *
 * Il contrasto è almeno 3 a 1 dal peggiore dei tre fondi del tema (`background`, `surface` e
 * `surfaceVariant` di `Theme.kt`), la soglia dei componenti non testuali: fra 3,01 e 3,50 col
 * tema chiaro, fra 5,7 e 6,7 con lo scuro. Sopra una copertina non c'è niente da garantire.
 * La coppia più vicina dista 0,042 in OkLab: il metro è la distanza percettiva, non lo scarto sui
 * canali.
 *
 * Non c'è un grigio, perché un grigio non è una tonalità (domanda `d-tinta-neutro`).
 * ⚠️ **Nell'archivio vive l'indice e non il colore** (`FolderTints`): cambiare l'ordine o i
 * colori cambia il colore delle cartelle già tinte.
 */
val FRONT_TINTS: List<FrontTint> = listOf(
    FrontTint(Color(0xFFD7534A), Color(0xFFFE8B7F)), // rosso
    FrontTint(Color(0xFFCF6000), Color(0xFFFE904E)), // corallo
    FrontTint(Color(0xFFB57500), Color(0xFFEF9D05)), // arancio
    FrontTint(Color(0xFF9D8201), Color(0xFFD1AE00)), // ambra
    FrontTint(Color(0xFF7F8F00), Color(0xFFAABE1E)), // oro
    FrontTint(Color(0xFF469B2C), Color(0xFF74CA5D)), // lime
    FrontTint(Color(0xFF009B6A), Color(0xFF06D190)), // verde
    FrontTint(Color(0xFF04998D), Color(0xFF03CCBB)), // smeraldo
    FrontTint(Color(0xFF0A96A4), Color(0xFF03C8DB)), // acqua
    FrontTint(Color(0xFF0091BF), Color(0xFF05C1FD)), // ciano
    FrontTint(Color(0xFF2286E5), Color(0xFF73B5FF)), // cielo
    FrontTint(Color(0xFF6878E8), Color(0xFF9AABFE)), // azzurro
    FrontTint(Color(0xFF916ADC), Color(0xFFBB9DFF)), // indaco
    FrontTint(Color(0xFFAF5EC3), Color(0xFFDF8BF3)), // viola
    FrontTint(Color(0xFFC554A1), Color(0xFFF782CF)), // magenta
    FrontTint(Color(0xFFD35078), Color(0xFFFE85A5))  // rosa
)

/**
 * La tinta scelta per una cartella nel tema in vigore, o `null` per quella dell'app.
 *
 * L'indice fuori elenco vale come 'nessuna scelta': l'elenco può accorciarsi, e un numero vecchio
 * nell'archivio non deve far andare in errore la schermata.
 * ⚠️ **Il tema è quello dell'app** ([LocalAivLight]), non quello di sistema, ed è per questo che
 * la funzione è composabile (`Rules.md` § '🌗 Il tema scelto DENTRO l'app non è quello di
 * sistema').
 */
@Composable
fun frontTintOf(index: Int?): Color? {
    val coppia = index?.let { FRONT_TINTS.getOrNull(it) } ?: return null
    return if (LocalAivLight.current) coppia.light else coppia.dark
}

/**
 * La finestra che fa scegliere la tinta di questa cartella.
 *
 * Non è una modale vera: non raccoglie input scritto, quindi il tocco fuori la chiude (`Rules.md`
 * § '👆 Che cosa fa il tocco FUORI da una finestra'). Il tocco su un tondo sceglie e chiude,
 * senza conferma: il colore si vede subito dietro la finestra.
 * Ogni tondo è un bersaglio solo con `Role.RadioButton`, e la sua descrizione parlata è il codice
 * esadecimale della variante del tema in vigore: un nome di colore andrebbe inventato e tradotto
 * in ventotto lingue.
 *
 * @param current l'indice scelto per questa cartella, o `null`.
 * @param onPick l'indice nuovo, o `null` per tornare alla tinta dell'app.
 */
@Composable
fun TintDialog(current: Int?, onPick: (Int?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(onDismiss),
        properties = loweredWindow(onDismiss),
        title = { Text(stringResource(R.string.front_tint)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TINT_GAP)) {
                FRONT_TINTS.chunked(TINT_COLUMNS).forEachIndexed { riga, tinte ->
                    Row(horizontalArrangement = Arrangement.spacedBy(TINT_GAP)) {
                        tinte.forEachIndexed { colonna, tinta ->
                            val quale = riga * TINT_COLUMNS + colonna
                            TintDot(
                                tint = tinta,
                                picked = quale == current,
                                onPick = { onPick(quale); onDismiss() }
                            )
                        }
                    }
                }
            }
        },
        /*
         * Il ritorno alla tinta dell'app è un tasto e non un diciassettesimo tondo: in fila con
         * gli altri si leggerebbe come una scelta in più, mentre toglie la scelta.
         */
        confirmButton = {
            TextButton(onClick = { onPick(null); onDismiss() }) {
                Text(stringResource(R.string.front_tint_none))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Un tondo del selettore, tagliato in due in orizzontale (richiesta dell'utente).
 *
 * ⚠️ **Sopra il tema chiaro e sotto quello scuro, con tutti e due i temi**: con la variante in
 * vigore sopra, lo stesso tondo si leggerebbe rovesciato da un tema all'altro.
 *
 * La spunta è dentro il tondo: su una tinta chiara un contorno d'accento si confonderebbe con la
 * tinta. Il suo inchiostro si sceglie dalla luminanza di ogni metà, non dal tema, quindi è fatta
 * di due mezze spunte: le due metà di un tondo possono volere una il bianco e l'altra il nero. Il
 * taglio della spunta coincide con quello del tondo per costruzione, perché il segno è centrato.
 */
@Composable
private fun TintDot(tint: FrontTint, picked: Boolean, onPick: () -> Unit) {
    val viva = if (LocalAivLight.current) tint.light else tint.dark
    val etichetta = "#%06X".format(viva.toArgb() and 0xFFFFFF)
    Box(
        modifier = Modifier
            .size(TINT_DOT)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .selectable(selected = picked, role = Role.RadioButton, onClick = onPick)
            .semantics { contentDescription = etichetta },
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.matchParentSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(tint.light))
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(tint.dark))
        }
        if (picked) {
            TintMark(tint.light, top = true)
            TintMark(tint.dark, top = false)
        }
    }
}

/** Metà della spunta, con l'inchiostro che si legge sulla metà di tondo che ha sotto. */
@Composable
private fun TintMark(sotto: Color, top: Boolean) {
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        tint = if (sotto.luminance() > TINT_DARK_INK) Color.Black else Color.White,
        modifier = Modifier
            .size(TINT_MARK)
            .drawWithContent {
                clipRect(
                    top = if (top) 0f else size.height / 2f,
                    bottom = if (top) size.height / 2f else size.height
                ) { this@drawWithContent.drawContent() }
            }
    )
}

/** Quanto è largo un tondo: il bersaglio minimo di un tocco. */
private val TINT_DOT = 48.dp

/** La spunta dentro il tondo. */
private val TINT_MARK = 24.dp

/** L'aria fra un tondo e l'altro, in tutte e due le direzioni. */
private val TINT_GAP = 8.dp

/** Quante colonne ha la griglia 4x4. */
private const val TINT_COLUMNS = 4

/**
 * Sopra quanta luminanza la spunta si scrive in nero.
 *
 * Mezzo, e non la soglia di contrasto WCAG: qui si sceglie fra due inchiostri opposti.
 */
private const val TINT_DARK_INK = 0.5f
